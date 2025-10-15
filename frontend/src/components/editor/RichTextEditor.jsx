import React from "react";
import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { Table } from "@tiptap/extension-table";
import { TableRow } from "@tiptap/extension-table-row";
import { TableCell } from "@tiptap/extension-table-cell";
import { TableHeader } from "@tiptap/extension-table-header";
import { Link } from "@tiptap/extension-link";
import { Image } from "@tiptap/extension-image";
import { TextAlign } from "@tiptap/extension-text-align";
import { Underline } from "@tiptap/extension-underline";
import { TextStyle } from "@tiptap/extension-text-style";
import { Color } from "@tiptap/extension-color";
import { Highlight } from "@tiptap/extension-highlight";
import { Youtube } from "@tiptap/extension-youtube";
import { Typography } from "@material-tailwind/react";
import MenuBar from "./MenuBar";
import "./editor.css";

const RichTextEditor = ({
  value = "",
  onChange,
  placeholder = "Nhập nội dung...",
  error,
  label,
  required = false,
  height = "400px",
}) => {
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: {
          levels: [1, 2, 3, 4, 5, 6],
        },
      }),
      Table.configure({
        resizable: true,
        cellMinWidth: 50,
        allowTableNodeSelection: true,
        HTMLAttributes: {
          class: "tiptap-table",
        },
      }),
      TableRow,
      TableHeader,
      TableCell.extend({
        addAttributes() {
          return {
            ...this.parent?.(),
            placeholder: {
              default: null,
              parseHTML: (element) => element.getAttribute("data-placeholder"),
              renderHTML: (attributes) => {
                if (!attributes.placeholder) {
                  return {};
                }
                return {
                  "data-placeholder": attributes.placeholder,
                };
              },
            },
          };
        },
      }),
      Link.configure({
        openOnClick: false,
        HTMLAttributes: {
          class: "tiptap-link",
          target: "_blank",
          rel: "noopener noreferrer",
        },
      }),
      Image.configure({
        HTMLAttributes: {
          class: "tiptap-image",
          inline: false,
          allowBase64: true,
        },
      }),
      Youtube.configure({
        width: 640,
        height: 480,
        nocookie: true,
      }),
      TextAlign.configure({
        types: ["heading", "paragraph"],
      }),
      Underline,
      TextStyle,
      Color,
      Highlight.configure({
        multicolor: true,
      }),
    ],
    content: value,
    editorProps: {
      attributes: {
        class: "tiptap-editor",
        "data-placeholder": placeholder,
      },
    },
    onUpdate: ({ editor }) => {
      const html = editor.getHTML();
      onChange(html);
    },
  });

  // Sync external value changes with editor
  React.useEffect(() => {
    if (editor && value !== editor.getHTML()) {
      try {
        editor.commands.setContent(value || "", false);
      } catch (error) {
        console.error("Error setting content:", error);
      }
    }
  }, [value, editor]);

  React.useEffect(() => {
    return () => {
      if (editor) {
        editor.destroy();
      }
    };
  }, [editor]);

  return (
    <div className="rich-text-editor-wrapper">
      {label && (
        <Typography
          variant="small"
          color="blue-gray"
          className="mb-2 font-medium"
        >
          {label} {required && <span className="text-red-500">*</span>}
        </Typography>
      )}

      <div className={`editor-container ${error ? "error" : ""}`}>
        <MenuBar editor={editor} />
        <div className="editor-content" style={{ minHeight: height }}>
          <EditorContent editor={editor} />
        </div>
      </div>

      {error && (
        <Typography variant="small" color="red" className="mt-2">
          {error}
        </Typography>
      )}

      {value && (
        <Typography
          variant="small"
          color="blue-gray"
          className="mt-2 opacity-60"
        >
          Số ký tự: {value.replace(/<[^>]+>/g, "").length} | Số từ:{" "}
          {
            value
              .replace(/<[^>]+>/g, "")
              .split(/\s+/)
              .filter(Boolean).length
          }
        </Typography>
      )}
    </div>
  );
};

export default RichTextEditor;
