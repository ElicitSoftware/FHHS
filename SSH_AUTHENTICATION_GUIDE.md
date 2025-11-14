# FHHS SFTP Authentication Guide

## Quick Setup

### Password Authentication (Simple)
```properties
family.history.sftp.host=your-server.com
family.history.sftp.username=your-username
family.history.sftp.password=your-password
family.history.sftp.path=/upload/path
family.history.sftp.port=22
```

### SSH Key Authentication (Recommended)
```properties
family.history.sftp.host=your-server.com
family.history.sftp.username=your-username
family.history.sftp.privateKey=/absolute/path/to/private/key
family.history.sftp.path=/upload/path
family.history.sftp.port=22
```

**Note:** SSH key takes precedence if both password and privateKey are set.

## SSH Key Setup

```bash
# 1. Generate key pair
ssh-keygen -t rsa -b 4096 -f fhhs_key -N ""

# 2. Copy public key to SFTP server
ssh-copy-id -i fhhs_key.pub username@your-server.com

# 3. Use private key in configuration
family.history.sftp.privateKey=/absolute/path/to/fhhs_key
```

## Private Key Options

### File Path
```properties
family.history.sftp.privateKey=/path/to/private/key
```

### Direct Content (for Docker/K8s)
```properties
family.history.sftp.privateKey=-----BEGIN OPENSSH PRIVATE KEY-----
...key content...
-----END OPENSSH PRIVATE KEY-----
```

### Environment Variable
```bash
export SFTP_PRIVATE_KEY="/path/to/key"
```
```properties
family.history.sftp.privateKey=${SFTP_PRIVATE_KEY}
```

## Common Issues & Solutions

### Key Not Working?
1. **Use password temporarily:**
   ```properties
   family.history.sftp.password=your-password
   # family.history.sftp.privateKey=/path/to/key
   ```

2. **Fix permissions:**
   ```bash
   chmod 600 /path/to/private/key
   ```

3. **Generate new key:**
   ```bash
   ssh-keygen -t rsa -b 4096 -m PEM -f new_key -N ""
   ```

### Test Connection
```bash
# Test manually first
ssh -i /path/to/key username@server
sftp -i /path/to/key username@server
```

### Debug Logging
```properties
quarkus.log.category."com.elicitsoftware.familyhistory.SftpService".level=DEBUG
```

## Example Configurations

### Development
```properties
%dev.family.history.sftp.host=localhost
%dev.family.history.sftp.username=fhhs_user
%dev.family.history.sftp.privateKey=/absolute/path/to/key
%dev.family.history.sftp.path=/upload/reports
```

### Production  
```properties
family.history.sftp.host=prod-server.com
family.history.sftp.username=fhhs_user
family.history.sftp.privateKey=${SFTP_PRIVATE_KEY}
family.history.sftp.path=/secure/upload
```