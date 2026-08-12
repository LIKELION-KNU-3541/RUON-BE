import tempfile
import unittest
from contextlib import contextmanager
from pathlib import Path
from unittest.mock import Mock, patch

from app.analysis import function_enrichment


class FunctionEnrichmentTest(unittest.TestCase):

    def test_updates_only_inci_functions_and_current_row(self):
        row = {"id": 7, "inci_name": "Niacinamide", "inci_functions": None}
        response = Mock(status_code=200)
        response.json.return_value = {
            "ingredient": {
                "functions": ["SKIN CONDITIONING", "ANTIOXIDANT", "antioxidant"]
            }
        }
        response.raise_for_status.return_value = None

        cursor = Mock()
        connection = Mock()
        connection.cursor.return_value = cursor

        @contextmanager
        def fake_connection():
            yield connection

        with tempfile.TemporaryDirectory() as temp_dir, \
                patch.object(function_enrichment.settings, "INCI_API_KEY", "test-key"), \
                patch.object(function_enrichment, "CACHE_PATH", Path(temp_dir) / "cache.json"), \
                patch.object(function_enrichment.requests, "get", return_value=response), \
                patch.object(function_enrichment, "get_conn", fake_connection):
            function_enrichment.enrich_missing_functions([row])

        self.assertEqual(
            "SKIN CONDITIONING, ANTIOXIDANT",
            row["inci_functions"],
        )
        sql = cursor.executemany.call_args.args[0]
        self.assertIn("SET inci_functions = %s", sql)
        self.assertNotIn("pregnancy_safe", sql)
        self.assertEqual(
            [("SKIN CONDITIONING, ANTIOXIDANT", 7)],
            cursor.executemany.call_args.args[1],
        )
        connection.commit.assert_called_once()


if __name__ == "__main__":
    unittest.main()
