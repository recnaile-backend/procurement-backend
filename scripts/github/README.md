# Recnaile GitHub MNC Bootstrap

Repository: `https://github.com/Recnaile/procurement-backend`
Organization: `Recnaile`

## Approval and ownership model

| Purpose | GitHub team | Mailbox |
|---|---|---|
| Production / `main` PR approval | `@Recnaile/production-approvers` | `info@recnaile.com` |
| Development + Staging PR approval | `@Recnaile/development-approvers` | `software@recnaile.com` |
| Backend developers | `@Recnaile/backend` | `software.backend@recnaile.com` |
| Frontend developers | `@Recnaile/frontend` | `software.frontend@recnaile.com` |

GitHub CODEOWNERS uses GitHub usernames/teams, not email addresses. The mailboxes above are the organization's notification/approval contacts. Add the corresponding GitHub accounts to the teams.

## Run

```powershell
gh auth login
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\github\setup-github.ps1 -Organization Recnaile -Repository procurement-backend
```

## Branch model

```text
feature/* / bugfix/* / hotfix/*
             |
             v
         staging
      Development + QA
             |
             v
           main
        Production
```

## PR policy

### `staging`
- PR required
- 1 approval minimum
- CODEOWNERS review required
- stale approvals dismissed after changes
- conversation resolution required
- no force-push
- no deletion
- approval group: `@Recnaile/development-approvers` (`software@recnaile.com`)

### `main`
- PR required
- 2 approvals minimum
- CODEOWNERS review required
- stale approvals dismissed after changes
- conversation resolution required
- no force-push
- no deletion
- approval group: `@Recnaile/production-approvers` (`info@recnaile.com`)

## Security
Never commit `.env`, MongoDB Atlas credentials, AWS access keys, private keys, certificates, tokens or production secrets. Use GitHub OIDC + AWS IAM/Secrets Manager for production deployments.
