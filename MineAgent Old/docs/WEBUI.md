# WebUI

Default URL:

```text
http://127.0.0.1:25712
```

Implemented endpoints:

- `GET /`
- `GET /api/status`
- `POST /api/instruct`
- `POST /api/stop`
- `GET /api/logs`
- `GET /api/packets?direction=BOTH&limit=100&filter=Entity`
- `POST /api/packets/send`
- `POST /api/custom-payload/send`

The UI is a dependency-free HTML page served by `com.sun.net.httpserver.HttpServer`.
