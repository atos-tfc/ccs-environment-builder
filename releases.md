# Slinky Environment Releases

[Home](README.md) > Releases

## Releases

### v1.0.0

- Initial version migrated from slinky-framework
- Add support for environment setup of Oracle DB using Liquibase

### v1.0.1

- Allow unlimited Oracle users to be dropped during Tear Down of Oracle DB
- Drop all Oracle public synonyms for users being dropped during Tear Down of Oracle DB
- Allow Oracle tablespaces to be dropped during Tear Down of Oracle DB

### v1.0.2

Make tearDown more robust
- Clean-up Liquibase DATABASECHANGELOG table after a tearDown (all change logs must start with specified prefix)
- Kill existing sessions cleanly
- Don't fail when dropping a tablespace that does not exist
