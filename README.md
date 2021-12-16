# Borough
The PCN Land Claim System

# Key Concepts of Borough
- Claim zones are sort of like plots. Each claim zone has it's own name and build permissions. Claim zones are made up of [Minecraft] chunks. These chunks can be all together,  -  spread out, or both. 

- Claim zones
- protect against block breaking, external liquid flow (water/lava), external fire, external piston actions, and external explosions. An 'external' action is something that is   triggered by someone or something that does not have build or destroy permissions in the claim zone.

- Claim Builders are users that have permission to build and destroy in a claim.

- Claim Moderators are users that have permission to build, destroy, and give and take other people's build permissions on that claim.

- Claim Administrators are users that have permission to build, destroy, give and take other people's build permissions on that claim, and /claim extend and /unclaim land on     that claim.

[Taken From https://www.peacefulcraft.net/flarum/d/196-how-to-land-claiming]
# Commands
/claim create [claim-name] creates a new claim zone called claim-name. The chunk you're standing in is automatically added to the claim.

/claim extend [claim-name] claims the chunk you're currently standing in, adding it to the claim with name claim-name.

/unclaim unclaims the chunk you're currently standing in.

/claim delete [claim-name] unclaims all chunks in claim claim-name and deletes the claim zone.

/claim info [claim-name] shows information about claim claim-name, such as user permissions and chunk locations.

/claim add-builder [claim-name] [username] grants username builder access to claim claim-name. (build and destroy permissions on blocks in the claim zone).

/claim add-moderator [claim-name] [username] grants username moderator access to claim claim-name. (builder, /claim add-builder, and /claim remove-builder).

/claim add-admin [claim-name] [username] grants username admin access to claim claim-name. (builder, mod, and /claim extend and /unclaim permissions).

claim remove-user [claim-name] [username] removes username's permissions to claim claim-name

[Taken From https://www.peacefulcraft.net/flarum/d/196-how-to-land-claiming]


# Permissions
[NEEDS UPDATE]

# Deveoper Usage
- This template is tailored of usage in VSCode. You should be prompted to install some VSCode extensions when you open the repo in VSCode to facilitate Java development. This template will still work in other IDEs and you can delete the `.vscode` folder if you don't plan to use VSCode.
- Install Java and Maven if you don't already have them installed. Google for tutorials on how to do this if you're not sure how.
- `pom.xml`
  - If not PCN, update `<groupId>`. Ex `me.parsonswy`
  - Update `<artifactId>`.
  - If using a different versioning notiation, update `<version>`
  - Update `<description>`
-  Project Structure
  - If not PCN update package structure to match your `<groudId>` Ex `me\parsonswy\templateus`
  - Change the `net.peacefulcraft.templateus` package to `[whatever you just set the package path to].[name of your plugin]` Ex `me.parsonswy.guishop`
  - Rename `Templateus.java` to match the name of your plugin. Ex `GuiShop.java`
  - Change `main\resources\plugin.yml` `main: net.peacefulcraft.templateus.Templateus` to match the path you just set above. Ex `me.parsonswy.guishop.GuiShop`. Note that the `.java` is left off here.
  - Moving / renaming in the above setups may result in compile errors in the `import` statements. Update any broken package paths accordingly.
  - Either remove of re-purpose the example command and permission in `plugin.yml`. See the Wiki article linked in `plugin.yml` for more details on what goes into `plugin.yml`.

# Compiling
- Open your OS' command terminal and navigate to this project's folder ( folder with `src`, `pom.xml`, `README.md`, etc ).
- Type `mvn package`
- Once the command is complete, there will be a jar file in `target\[<artifactId>]-[version].jar`

  For questions, comments, or suggestions on this repo or Bukkit plugin development in general:
  - https://spigotmc.org
  - https://bukkit.org
  - https://www.peacefulcraft.net/flarum/t/github
