---
title: "get_url_host"
sidebar_label: "get_url_host"
id: "get_url_host"
description: "This macro extracts a hostname from a column containing a url."
category: "Web macros"
---

This macro extracts a hostname from a column containing a url.

**Usage:**

```sql
{{ dbt_utils.get_url_host(field='page_url') }}
```
