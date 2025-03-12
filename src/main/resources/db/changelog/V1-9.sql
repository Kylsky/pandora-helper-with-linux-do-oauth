update share_gpt_config set expires_at  = null where expires_at = '-';
update share_claude_config set expires_at  = null where expires_at = '-';
update share_api_config set expires_at  = null where expires_at = '-';
update share_grok_config set expires_at  = null where expires_at = '-';