"""
dbt model compilation script for Datamancer IntelliJ plugin.
Outputs JSON for reliable parsing by the Kotlin runtime.
"""
import io
import json
import sys
from contextlib import redirect_stdout


def main():
    if len(sys.argv) < 2:
        print(json.dumps({"success": False, "error": "Please provide a model name to compile."}))
        sys.exit(1)

    model_name = sys.argv[1]

    try:
        from dbt.cli.main import dbtRunner, dbtRunnerResult

        dbt = dbtRunner()

        cli_args = [
            "compile",
            "--select", model_name,
            "--no-version-check",
            "--no-introspect",
            "--quiet",
            "--no-print",
            "--output",
            "json"
        ]

        # dbt prints to stdout, so capture and discard it
        buffer = io.StringIO()
        with redirect_stdout(buffer):
            res: dbtRunnerResult = dbt.invoke(cli_args)

        if res.result and len(res.result) >= 1:
            node = res.result[0].node
            if hasattr(node, "compiled_code") and node.compiled_code:
                result = {
                    "success": True,
                    "compiled_code": node.compiled_code,
                }
                if hasattr(node, "compiled_path") and node.compiled_path:
                    result["compiled_path"] = node.compiled_path
                if hasattr(node, "original_file_path") and node.original_file_path:
                    result["compiled_full_path"] = node.original_file_path
                print(json.dumps(result))
                sys.exit(0)
            elif hasattr(node, "status") and node.status == "error":
                error_msg = getattr(node, "error", "Unknown compilation error")
                print(json.dumps({"success": False, "error": str(error_msg)}))
                sys.exit(1)
            else:
                print(json.dumps({"success": False, "error": "No compiled code in result"}))
                sys.exit(1)
        else:
            if res.exception:
                print(json.dumps({"success": False, "error": str(res.exception)}))
            else:
                print(json.dumps({"success": False, "error": "No result from dbt compilation"}))
            sys.exit(1)

    except ImportError as e:
        print(json.dumps({"success": False, "error": f"dbt is not installed: {e}"}))
        sys.exit(1)
    except Exception as e:
        print(json.dumps({"success": False, "error": str(e)}))
        sys.exit(1)


if __name__ == "__main__":
    main()
