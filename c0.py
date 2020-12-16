import sys

def main():
    with open(sys.argv[1], 'r', encoding='utf-8') as fp:
        print(fp.read(), file=sys.stderr)


if __name__ == '__main__':
    main()
