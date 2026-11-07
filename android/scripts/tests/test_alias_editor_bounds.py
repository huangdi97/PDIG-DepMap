"""Regression for R60: Compose AlertDialog's actual EditText in UIAutomator.

This test intentionally includes the bounds and node shape captured from the
failed API36 Phone Pixel run 38523883281 (source 165cd17b). It must fail if
someone reintroduces r"\\\\d+" or treats the label as the editable control.
"""
import sys
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ui_vnext_pixel_capture import find_single_alias_editor, parse_android_bounds


class AliasEditorBoundsTest(unittest.TestCase):
    def test_real_api36_rect_and_edittext_are_found(self):
        self.assertEqual(
            (166, 1048, 914, 1224),
            parse_android_bounds("[166,1048][914,1224]"),
        )
        xml = """<root>
          <node class="android.widget.TextView" text="给号码命名" bounds="[166,908][496,1004]" />
          <node class="android.widget.EditText" text="" focused="false" bounds="[166,1048][914,1224]">
            <node class="android.widget.TextView" text="自定义名称（可留空）" bounds="[210,1114][664,1180]" />
          </node>
          <node class="android.widget.TextView" text="保存名称" bounds="[727,1406][881,1461]" />
        </root>"""
        nodes = list(ET.fromstring(xml).iter("node"))
        actual = find_single_alias_editor(nodes)
        self.assertIsNotNone(actual)
        node, rect = actual
        self.assertEqual("android.widget.EditText", node.get("class"))
        self.assertEqual((540, 1136), ((rect[0]+rect[2])//2, (rect[1]+rect[3])//2))

    def test_reject_malformed_or_nonpositive_bounds(self):
        for value in ("", None, "[166,1048][166,1224]",
                      "[166,1048][914,1048]", "166,1048,914,1224",
                      "[166,1048][914,1224]extra"):
            with self.subTest(value=value):
                self.assertIsNone(parse_android_bounds(value))

    def test_fail_closed_if_no_editable_or_more_than_one(self):
        label = ET.Element("node", {"class": "android.widget.TextView",
                                    "bounds": "[166,1048][914,1224]"})
        editor = ET.Element("node", {"class": "android.widget.EditText",
                                     "bounds": "[166,1048][914,1224]"})
        self.assertIsNone(find_single_alias_editor([label]))
        self.assertIsNone(find_single_alias_editor([editor, editor]))
        self.assertIsNotNone(find_single_alias_editor([label, editor]))


if __name__ == "__main__":
    unittest.main()
