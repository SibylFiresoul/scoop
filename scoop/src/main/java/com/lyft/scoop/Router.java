package com.lyft.scoop;

public abstract class Router {

    private ScoopBackstack backStack = new ScoopBackstack();
    private ScreenScooper screenScooper;
    private Scoop root;

    public Router(ScreenScooper screenScooper) {

        this.screenScooper = screenScooper;
    }

    public boolean goBack() {
        if (!backStack.isEmpty()) {
            Scoop previousScoop = backStack.peek();
            Screen previousScreen = Screen.fromScoop(previousScoop);

            backStack.pop();

            if (!backStack.isEmpty()) {
                Scoop nextScoop = backStack.peek();
                Screen nextScreen = Screen.fromScoop(nextScoop);
                performScoopChange(nextScoop, nextScreen, previousScreen, TransitionDirection.EXIT);

                return true;
            }
        }

        return false;
    }

    public void goUp() {
        if (!backStack.isEmpty()) {
            Scoop scoop = backStack.peek();

            Class<? extends ViewController> controller = Screen.fromScoop(scoop).getController();

            if (Utils.hasAnnotation(controller, ParentController.class)) {
                Screen screen = Screen.create(controller.getAnnotation(ParentController.class).value());
                resetTo(screen);
            } else {
                throw new RuntimeException("ParentController annotation not specified for this controller: " + controller.getSimpleName());
            }
        }
    }

    public void goTo(Screen screen) {
        Scoop previousScoop = backStack.peek();
        Screen previousScreen = Screen.fromScoop(previousScoop);

        if (sameController(Screen.fromScoop(previousScoop), screen)) {
            return;
        }

        Scoop nextScoop = screenScooper.createScreenScoop(screen, previousScoop);
        backStack.push(nextScoop);
        performScoopChange(nextScoop, screen, previousScreen, TransitionDirection.ENTER);
    }

    public void replaceWith(Screen screen) {
        Scoop previousScoop = backStack.peek();
        Screen previousScreen = Screen.fromScoop(previousScoop);

        if (sameController(Screen.fromScoop(previousScoop), screen)) {
            return;
        }

        Scoop nextScoop;

        if (!backStack.isEmpty()) {
            Scoop previousParent = previousScoop.getParent();

            backStack.pop();

            nextScoop = screenScooper.createScreenScoop(screen, previousParent);
        } else {
            nextScoop = screenScooper.createScreenScoop(screen, root);
        }

        backStack.push(nextScoop);
        performScoopChange(nextScoop, screen, previousScreen, TransitionDirection.ENTER);
    }

    public void resetTo(Screen screen) {
        resetTo(screen, TransitionDirection.EXIT);
    }

    public void resetTo(Screen screen, TransitionDirection direction) {
        Scoop previousScoop = backStack.peek();
        Screen previousScreen = Screen.fromScoop(previousScoop);

        while (!backStack.isEmpty()) {
            Scoop topScoop = backStack.peek();

            if (sameController(screen, Screen.fromScoop(topScoop))) {
                performScoopChange(topScoop, screen, previousScreen, direction);
                return;
            }

            backStack.pop();
        }

        Scoop nextScoop = screenScooper.createScreenScoop(screen, root);
        backStack.push(nextScoop);
        performScoopChange(nextScoop, screen, previousScreen, direction);
    }

    public void onCreate(Scoop root, Screen defaultScreen) {
        this.root = root;

        if (backStack.isEmpty()) {
            backStack.push(screenScooper.createScreenScoop(defaultScreen, root));
        }

        performScoopChange(backStack.peek(), defaultScreen, null, TransitionDirection.ENTER);
    }

    private void performScoopChange(Scoop scoop, Screen next, Screen previous, TransitionDirection direction) {
        onScoopChanged(new RouteChange(scoop, previous, next, direction));
    }

    protected abstract void onScoopChanged(RouteChange routeChange);

    static boolean sameController(Screen previous, Screen next) {

        if (previous == null || next == null) {
            return false;
        }

        return previous.getController().equals(next.getController());
    }
}
