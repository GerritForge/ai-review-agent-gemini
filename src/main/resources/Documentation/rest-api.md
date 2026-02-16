Rest API
========

## User Gemini API Key Endpoints

This plugin exposes REST endpoints to allow users to manage their personal
Gemini API key. The API key is stored per-user.

All endpoints require authentication.

---

### Set / Update Gemini API Key

**PUT** `/a/accounts/self/geminiToken`

Sets or updates the Gemini API key for the current user.

#### Request Body

```json
{
  "token": "your-gemini-api-key"
}
```

#### Response

- `200 Ok` on success.

If the key is empty or invalid, the request will fail with:

- `400 Bad Request`

---

## Security Considerations

- Only the authenticated user can manage their own key.
