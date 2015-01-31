## API Documentation
- Table of Contents
  - [GET /api/cats](#user-content-get-apicats)

## GET /api/cats
get all cats

#### Request
```
GET /api/cats HTTP/1.1
```

#### Response
```
200
Content-Type: application/json; charset=utf-8

{
  "cats" : [ {
    "name" : "Tama",
    "color" : "white"
  }, {
    "name" : "Boss",
    "color" : "brown"
  } ]
}
```
