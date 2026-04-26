---
title: "pretty_time"
sidebar_label: "pretty_time"
id: "pretty_time"
description: "This macro returns a string of the current timestamp, optionally taking a datestring format."
category: "Jinja Helpers"
---

This macro returns a string of the current timestamp, optionally taking a datestring format.

```sql
{#- This will return a string like '14:50:34' -#}
{{ dbt_utils.pretty_time() }}

{#- This will return a string like '2019-05-02 14:50:34' -#}
{{ dbt_utils.pretty_time(format='%Y-%m-%d %H:%M:%S') }}
```
