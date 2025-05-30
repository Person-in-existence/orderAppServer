package orderApp.Server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class CommandRegistry {
    private static final ArrayList<Command> commands = new ArrayList<>(List.of(
            new Command("end", Main::end),
            new Command("exit", Main::exit),
            new Command("help", CommandRegistry::listAllCommands),
            new Command("data", Main::data),
            new Command("start", Main::start),
            new Command("debug", Main::debug),
            new Command("setname", Main::setName)
    ));
    public static synchronized void registerCommand(String name, CommandCaller function) {
        commands.add(new Command(name, function));
    }
    public static void handleCommand(String input) {
        try{
            String[] parts = input.split(" ");
            String[] args = Arrays.copyOfRange(parts, 1, parts.length);
            String keyword = parts[0];

            // Try and find the command
            boolean commandFound = false;
            for (Command command: commands) {
                if (Objects.equals(command.name, keyword)) {
                    commandFound = true;
                    command.caller.call(args);
                }
            }
            // If the command couldn't be found, tell the user no such command exists
            if (!commandFound) {
                System.err.println("Command \"" + keyword + "\" does not exist. Use \"help\" to list all commands");
            }
        } catch (Exception e) {
            System.err.println("Unable to handle command. Please try again.");
            if (Main.debug) {
                System.err.println(e.getClass());
                System.err.println(e.getMessage());
                System.err.println(Arrays.toString(e.getStackTrace()));
            }
        }

    }

    private record Command(String name, CommandCaller caller) {
    }
    public interface CommandCaller {
        void call(String[] args);
    }
    public static void listAllCommands(String[] ignored) {
        for (Command command: commands) {
            Main.reader.printAbove("\t" + command.name);
        }
    }
}
