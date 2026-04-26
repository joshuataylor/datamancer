---
title: "pretty_log_format"
sidebar_label: "pretty_log_format"
id: "pretty_log_format"
description: "This macro formats the input in a way that will print nicely to the command line when you `log` it."
category: "Jinja Helpers"
---

This macro formats the input in a way that will print nicely to the command line when you `log` it.

```sql
{#- This will return a string like:
"11:07:31 + my pretty message"
-#}

{{ dbt_utils.pretty_log_format("my pretty message") }}
```
