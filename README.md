# Configuration

## AWS resources

Create these before starting the backend (names are configurable):

| Resource | Name | Notes |
|---|---|---|
| S3 bucket | `vanishvault-files` | Stores encrypted file bytes under `uploads/{fileId}/{filename}` |
| DynamoDB table | `vanishvault-files` | Partition key `fileId` (String) |
| DynamoDB table | `vanishvault-logs` | Partition key `logId` (String), plus a global secondary index named `fileId-index` on `fileId` |
| SES (optional) | | Verify a sender address and update the `source` in `EmailService` |

The IAM user needs `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject` on the bucket and `dynamodb:PutItem`, `GetItem`, `UpdateItem`, `DeleteItem`, `Query` on both tables, plus `ses:SendEmail` if email alerts are used.

## Backend (`src/main/resources/application.yml`)

Copy `application.yml.example` and fill in:

| Key | Default | Description |
|---|---|---|
| `aws.access-key` | | IAM access key |
| `aws.secret-key` | | IAM secret key |
| `aws.region` | `ap-south-1` | AWS region |
| `aws.s3.bucket-name` | `vanishvault-files` | S3 bucket |
| `aws.dynamodb.table-name` | `vanishvault-files` | Metadata table |
| `aws.dynamodb.logs-table-name` | `vanishvault-logs` | Access log table |
| `app.base-url` | `http://localhost:5173` | Frontend origin used to build share links |
| `server.port` | `8080` | |
| `spring.servlet.multipart.max-file-size` | `100MB` | Upload size cap |

`application.yml` contains secrets and is git-ignored; only the `.example` file is committed.

## Frontend

| File | Variable | Value |
|---|---|---|
| `frontend/.env.development` | `VITE_API_URL` | `http://localhost:8080` |
| `frontend/.env.production` | `VITE_API_URL` | Your deployed backend URL |

In development, Vite also proxies `/api` to `http://localhost:8080`.

Allowed CORS origins are listed in `CorsConfig.java` (`http://localhost:5173` and the production frontend). Add yours there if you deploy elsewhere.

## Deployment

The project is set up for the backend on Railway and the frontend on Vercel. Set `app.base-url` to the Vercel URL, `VITE_API_URL` in `.env.production` to the Railway URL, and add the Vercel origin to `CorsConfig`.

## How access works

1. On upload, a random AES-256 key is generated, the file is encrypted, and the ciphertext goes to S3. The bcrypt password hash, the Base64 key, the TTL and the limits are saved in DynamoDB.
2. On verify, the backend checks in order: TTL, IP lock, download limit, password. Any failure that should destroy the file deletes it from S3 and DynamoDB and notifies the sender.
3. On a correct password, the ciphertext is fetched, decrypted in memory and streamed back. If burn-after-read is on, the file is deleted immediately after.
4. Every check writes an access log row and pushes a STOMP message to `/user/{senderId}/queue/alerts`.

## Known limitations

- **No authentication.** `senderId` is a plain form field (the frontend hardcodes it), and revoke and log endpoints are open to anyone who knows a file ID.
- **Key stored with metadata.** The AES key lives in DynamoDB next to the file record, so anyone with database access can decrypt files. A KMS-managed key would be the next step.
- **Cipher mode.** `Cipher.getInstance("AES")` resolves to ECB with no IV. Switching to AES-GCM would add integrity and avoid pattern leakage in large files.
- **Expiry is lazy.** Expired files are deleted only when someone tries to open them; enable DynamoDB TTL on the `ttl` attribute and an S3 lifecycle rule for true cleanup.
- **IP detection** relies on the `X-Forwarded-For` header, which is only trustworthy behind a proxy you control.
