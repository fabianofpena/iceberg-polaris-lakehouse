from interface.cli import parse_args
from main.polaris_controller import PolarisController

def main():
    args = parse_args()
    controller = PolarisController(args)
    controller.execute()

if __name__ == "__main__":
    main()
