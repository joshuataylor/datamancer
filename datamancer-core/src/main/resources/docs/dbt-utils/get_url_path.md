---
title: "get_url_path"
sidebar_label: "get_url_path"
id: "get_url_path"
description: "This macro extracts a page path from a column containing a url."
category: "Web macros"
---

This macro extracts a page path from a column containing a url.

**Usage:**

```sql
{{ dbt_utils.get_url_path(field='page_url') }}
```
