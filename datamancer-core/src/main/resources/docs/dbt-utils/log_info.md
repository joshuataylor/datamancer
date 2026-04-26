---
title: "log_info"
sidebar_label: "log_info"
id: "log_info"
description: "This macro logs a formatted message (with a timestamp) to the command line."
category: "Jinja Helpers"
---

This macro logs a formatted message (with a timestamp) to the command line.

```sql
{{ dbt_utils.log_info("my pretty message") }}
```

```shell
11:07:28 | 1 of 1 START table model analytics.fct_orders........................ [RUN]
11:07:31 + my pretty message
```
