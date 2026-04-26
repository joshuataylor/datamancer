---
title: "get_url_parameter"
sidebar_label: "get_url_parameter"
id: "get_url_parameter"
description: "This macro extracts a url parameter from a column containing a url."
category: "Web macros"
---

This macro extracts a url parameter from a column containing a url.

**Usage:**

```sql
{{ dbt_utils.get_url_parameter(field='page_url', url_parameter='utm_source') }}
```
