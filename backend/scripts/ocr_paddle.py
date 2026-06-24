#!/usr/bin/env python3
"""PaddleOCR bridge for military-doc-sandbox."""
import sys, os, argparse, traceback

def main():
    p = argparse.ArgumentParser()
    p.add_argument("image")
    p.add_argument("--lang", default="ch")
    args = p.parse_args()

    if not os.path.exists(args.image):
        sys.stderr.write(f"ERROR: not found: {args.image}\n")
        sys.exit(1)

    try:
        from paddleocr import PaddleOCR
        ocr = PaddleOCR(lang=args.lang)
        result = ocr.ocr(args.image)

        if result is None or len(result) == 0 or result[0] is None:
            sys.stdout.write("")
            sys.exit(0)

        lines = []
        for li in result[0]:
            if li is None:
                continue
            text = li[1][0] if li[1] else ""
            conf = li[1][1] if li[1] and len(li[1]) > 1 else 0
            if conf > 0.5 and text.strip():
                lines.append(text)

        sys.stdout.write("\n".join(lines))
        sys.exit(0)

    except ImportError:
        sys.stderr.write("ERROR: PaddleOCR not installed\n")
        sys.exit(2)
    except Exception as e:
        sys.stderr.write(f"ERROR: {e}\n")
        traceback.print_exc(file=sys.stderr)
        sys.exit(3)

if __name__ == "__main__":
    main()
