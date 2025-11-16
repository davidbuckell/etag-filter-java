# entity-tags-demo
A prototype demonstrating the use of entity tags to perform cache busting when requested content has been modified.

Install, build & run the application.

In a new terminal window make an initial CURL request to:

`curl -i http://localhost:8080/items/1 -w '\n'`.

This will return the full request body including an `ETag` header.
```
HTTP/1.1 200 OK
Content-Type: application/json
Date: Sun, 16 Nov 2025 23:00:19 GMT
ETag: "MS1LZXlib2FyZC00OS45OS0wIG1pbnMgcGFzdCB0aGUgaG91cg=="
Transfer-Encoding: chunked
```

The following script will continuously ping the service every second passing the ETag argument within the `if-none-match` request header.

```
#!/bin/bash

ETAG_HEADER="$1"
URL="http://localhost:3000/items/1"

while true; do
  RESPONSE=$(curl -s -i -H "If-None-Match: \"$ETAG_HEADER\"" "$URL")

  STATUS=$(echo "$RESPONSE" | head -n1 | awk '{print $2}')
  RESPONSE_ETAG=$(echo "$RESPONSE" | grep -i '^ETag:' | awk '{print $2}' | tr -d '\r')

  # Only print ETag if non-empty
  if [ -n "$RESPONSE_ETAG" ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') Status: $STATUS ETag: $RESPONSE_ETAG"
  else
    echo "$(date '+%Y-%m-%d %H:%M:%S') Status: $STATUS"
  fi

  # Print body for 200 responses
  if [ "$STATUS" = "200" ]; then
    BODY=$(echo "$RESPONSE" | sed -n '/^\s*$/,$p' | tail -n +2)
    echo "$BODY"
  fi

  echo "----------------------------------------"
  sleep 1
done
```

Ensure the script is executable via:

`chmod +x client.sh`

Then execute the script passing in the correct ETag value
```
./client.sh "NDMgbWlucyBwYXN0IHRoZSBob3VyLTEtcGtvYmI3"
```

Whilst within the same minute, the response status code will be 304 with no response body :
```
2025-11-16 23:00:50 Status: 304

----------------------------------------
```

As soon as the current minute has elapsed the response will revert to the original 200 with response body until the new ETag value is passed within the request header.

Example output:

`curl -i http://localhost:8080/items/1 -w '\n'`

```
HTTP/1.1 200 OK
Content-Type: application/json
Date: Sun, 16 Nov 2025 23:00:19 GMT
ETag: "MS1LZXlib2FyZC00OS45OS0wIG1pbnMgcGFzdCB0aGUgaG91cg=="
Transfer-Encoding: chunked```

The following script will continuously ping the service every second passing the ETag argument within the `if-none-match` request header.
```

`./client.sh "MS1LZXlib2FyZC00OS45OS0wIG1pbnMgcGFzdCB0aGUgaG91cg=="`

```
2025-11-16 23:00:58 Status: 304

----------------------------------------
2025-11-16 23:00:59 Status: 304

----------------------------------------
2025-11-16 23:01:00 Status: 200 ETag: "MS1LZXlib2FyZC00OS45OS0xIG1pbnMgcGFzdCB0aGUgaG91cg=="
{"etag":"\"MS1LZXlib2FyZC00OS45OS0xIG1pbnMgcGFzdCB0aGUgaG91cg==\"","id":1,"name":"Keyboard","price":49.99,"updatedAt":"1 mins past the hour"}

----------------------------------------
2025-11-16 23:01:02 Status: 200 ETag: "MS1LZXlib2FyZC00OS45OS0xIG1pbnMgcGFzdCB0aGUgaG91cg=="
{"etag":"\"MS1LZXlib2FyZC00OS45OS0xIG1pbnMgcGFzdCB0aGUgaG91cg==\"","id":1,"name":"Keyboard","price":49.99,"updatedAt":"1 mins past the hour"}

----------------------------------------
```

Although this does not work out of the box.

The initial response appears to contain an invalid ETag.

```
HTTP/1.1 200 
ETag: "076395f9cad3643020114369263b34e90"
Content-Type: application/json
Content-Length: 76
Date: Sun, 16 Nov 2025 23:31:12 GMT
```

Attempting to pass this into the script returns 200, with the correct ETag.

Stopping & restarting the script with the new ETag does then work.

```
david@david-XPS-8700:~/source/repositories/etag-filter-java$ ./client.sh "076395f9cad3643020114369263b34e90"
2025-11-16 23:31:26 Status: 200 ETag: "MS1LZXlib2FyZC00OS45OS0zMSBtaW5zIHBhc3QgdGhlIGhvdXI="
{"etag":"\"MS1LZXlib2FyZC00OS45OS0zMSBtaW5zIHBhc3QgdGhlIGhvdXI=\"","id":1,"name":"Keyboard","price":49.99,"updatedAt":"31 mins past the hour"}
----------------------------------------

```

According to ChatGPT:

Ah — I see exactly what’s happening. The ShallowEtagHeaderFilter isn’t having any effect in your setup because of how Spring Boot handles filters and response bodies. Let me explain and show the fix.

1️⃣ Why your current filter isn’t working

You have:

    @Bean
    public Filter shallowEtagHeaderFilter() {
        return new ShallowEtagHeaderFilter();
    }

This registers a filter in Spring’s filter chain. BUT:

1. ShallowEtagHeaderFilter only works for responses that are written to the servlet output stream (i.e., the response must be committed).

2. In Spring Boot with @RestController returning objects, Spring uses MappingJackson2HttpMessageConverter to serialize objects to JSON.

3. ShallowEtagHeaderFilter wraps the response, but it sometimes runs before the body is written, especially if you return an object instead of a ResponseEntity with Content-Length.

Therefore, the ETag header never appears or always changes.

2️⃣ What you need to do

You have 2 solid options:

Option A — Return a ResponseEntity with body and let Spring handle ETag

Spring 5+ supports ETag headers automatically if you return a ResponseEntity with ETag set. Example:

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItem(@PathVariable int id,
                                    @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        Item item = itemsService.findOne(id);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String etag = itemsService.computeETag(item); // your custom ETag logic

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .body(item);
    }

This is the most reliable way. You compute ETag yourself and return it, which works better than ShallowEtagHeaderFilter for JSON responses.

Option B — Configure ShallowEtagHeaderFilter correctly

If you still want to use the filter:

Register it with FilterRegistrationBean so it applies to dispatcher servlet requests:

    import org.springframework.boot.web.servlet.FilterRegistrationBean;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.web.filter.ShallowEtagHeaderFilter;

    @Configuration
    public class EtagConfig {

        @Bean
        public FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
            FilterRegistrationBean<ShallowEtagHeaderFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new ShallowEtagHeaderFilter());
            registration.addUrlPatterns("/items/*"); // or "/*" for all
            registration.setName("etagFilter");
            registration.setOrder(1);
            return registration;
        }
    }


Make sure your controller does not commit the response early, and the response body is buffered (Spring Boot usually does this).

⚠ Note: ShallowEtagHeaderFilter works better for static content or String responses. For JSON objects, sometimes it does not work reliably because Jackson streams the body differently.

3️⃣ Recommendation

If you want robust, correct HTTP caching with ETags, Option A is best: compute ETag in the service/controller and return a ResponseEntity with .eTag() and .status(HttpStatus.NOT_MODIFIED) if it matches If-None-Match.

ShallowEtagHeaderFilter is convenient for serving static resources, not complex REST JSON APIs.

✅ Updated Controller Example

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItem(
        @PathVariable int id,
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {

        Item item = itemsService.findOne(id);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String etag = itemsService.computeETag(item);

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .body(item);
    }

Other explanation:

1️⃣ Does Spring Boot automatically add ETags for JSON responses?

No. Spring Boot does not automatically compute ETags for your REST endpoints that return JSON objects.

The ShallowEtagHeaderFilter exists, but it only works under specific conditions:

The response body must be fully written to the servlet output stream.

It works best for String responses or static content.

For JSON objects returned via @RestController, Spring writes them using HttpMessageConverter (Jackson), and the filter may not compute a stable ETag correctly, especially for objects serialized to JSON — the serialized bytes may differ slightly between requests.

2️⃣ What ShallowEtagHeaderFilter actually does

It buffers the response, calculates a hash of the body, and sets the ETag header.

Works reliably for HTML, text, or byte arrays.

With @RestController returning a POJO (like your Item), the JSON serialization may cause the hash to change between requests even if the object hasn’t changed, or the filter may run too early.

So just returning Item does not guarantee a proper ETag header will appear.
