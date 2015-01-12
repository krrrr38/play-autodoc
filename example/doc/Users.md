
## GET /api/users
get all users

### Example

#### Request
```
GET /api/users HTTP/1.1
```

#### Response
```
200
Content-Type: application/json; charset=utf-8

{
  "users" : [ {
    "name" : "Bob",
    "height" : 172
  }, {
    "name" : "Alice",
    "height" : 155
  } ]
}
```

## GET /api/users/:name
find user

### Example

#### Request
```
GET /api/users/yuno HTTP/1.1
```

#### Response
```
200
Content-Type: application/json; charset=utf-8

{
  "user" : {
    "name" : "yuno",
    "height" : 163
  }
}
```

## POST /api/users
create user

### Example

#### Request
```
POST /api/users HTTP/1.1
X-API-Token: YOUR_API_TOKEN
X-Public-Token: non-secret

{
  "name" : "yuno",
  "height" : 144
}
```

#### Response
```
201
Content-Type: application/json; charset=utf-8

{
  "user" : {
    "name" : "yuno",
    "height" : 144
  }
}
```
