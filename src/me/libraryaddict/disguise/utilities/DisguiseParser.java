package me.libraryaddict.disguise.utilities;

import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import me.libraryaddict.disguise.DisguiseConfig;
import me.libraryaddict.disguise.disguisetypes.*;
import org.bukkit.Art;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

public class DisguiseParser {
    public static class DisguiseParseException extends Exception {
        private static final long serialVersionUID = 1276971370793124510L;

        public DisguiseParseException() {
            super();
        }

        public DisguiseParseException(LibsMsg message, String... params) {
            super(message.get((Object[]) params));
        }
    }

    public static class DisguisePerm {
        private DisguiseType disguiseType;
        private String permName;

        public DisguisePerm(DisguiseType disguiseType) {
            this.disguiseType = disguiseType;
        }

        public DisguisePerm(DisguiseType disguiseType, String disguisePerm) {
            this.disguiseType = disguiseType;
            permName = disguisePerm;
        }

        public Class getEntityClass() {
            return getType().getEntityClass();
        }

        public EntityType getEntityType() {
            return getType().getEntityType();
        }

        public DisguiseType getType() {
            return disguiseType;
        }

        public Class<? extends FlagWatcher> getWatcherClass() {
            return getType().getWatcherClass();
        }

        public boolean isMisc() {
            return getType().isMisc();
        }

        public boolean isMob() {
            return getType().isMob();
        }

        public boolean isPlayer() {
            return getType().isPlayer();
        }

        public boolean isUnknown() {
            return getType().isUnknown();
        }

        public String toReadable() {
            return permName == null ? getType().toReadable() : permName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((disguiseType == null) ? 0 : disguiseType.hashCode());
            result = prime * result + ((permName == null) ? 0 : permName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;

            if (obj == null)
                return false;

            if (!(obj instanceof DisguisePerm))
                return false;

            DisguisePerm other = (DisguisePerm) obj;

            if (disguiseType != other.disguiseType)
                return false;

            if (permName == null) {
                if (other.permName != null)
                    return false;
            } else if (!permName.equals(other.permName))
                return false;

            return true;
        }
    }

    private static Object callValueOf(Class<?> param, String valueString,
            String methodName) throws DisguiseParseException {
        Object value;
        try {
            value = param.getMethod("valueOf", String.class).invoke(null, valueString.toUpperCase());
        }
        catch (Exception ex) {
            throw parseToException(param, valueString, methodName);
        }
        return value;
    }

    private static void doCheck(CommandSender sender, HashMap<ArrayList<String>, Boolean> optionPermissions,
            ArrayList<String> usedOptions) throws DisguiseParseException {

        if (!passesCheck(sender, optionPermissions, usedOptions)) {
            throw new DisguiseParseException(LibsMsg.D_PARSE_NOPERM, usedOptions.get(usedOptions.size() - 1));
        }
    }

    private static HashMap<String, Boolean> getDisguiseOptions(CommandSender sender, String permNode,
            DisguisePerm type) {
        switch (type.getType()) {
            case PLAYER:
            case FALLING_BLOCK:
            case PAINTING:
            case SPLASH_POTION:
            case FISHING_HOOK:
            case DROPPED_ITEM:
                HashMap<String, Boolean> returns = new HashMap<>();

                String beginning = "libsdisguises.options." + permNode + ".";

                for (PermissionAttachmentInfo permission : sender.getEffectivePermissions()) {
                    String lowerPerm = permission.getPermission().toLowerCase();

                    if (lowerPerm.startsWith(beginning)) {
                        String[] split = lowerPerm.substring(beginning.length()).split("\\.");

                        if (split.length > 1) {
                            if (split[0].replace("_", "").equals(type.toReadable().toLowerCase().replace(" ", ""))) {
                                for (int i = 1; i < split.length; i++) {
                                    returns.put(split[i], permission.getValue());
                                }
                            }
                        }
                    }
                }

                return returns;
            default:
                return new HashMap<>();
        }
    }

    public static DisguisePerm getDisguisePerm(String name) {
        for (DisguisePerm perm : getDisguisePerms()) {
            if (!perm.toReadable().replaceAll("[ |_]", "").equalsIgnoreCase(name.replaceAll("[ |_]", "")))
                continue;

            return perm;
        }

        if (name.equalsIgnoreCase("p"))
            return getDisguisePerm(DisguiseType.PLAYER.toReadable());

        return null;
    }

    public static DisguisePerm[] getDisguisePerms() {
        DisguisePerm[] perms = new DisguisePerm[DisguiseType.values().length +
                DisguiseConfig.getCustomDisguises().size()];
        int i = 0;

        for (DisguiseType disguiseType : DisguiseType.values()) {
            perms[i++] = new DisguisePerm(disguiseType);
        }

        for (Entry<String, Disguise> entry : DisguiseConfig.getCustomDisguises().entrySet()) {
            perms[i++] = new DisguisePerm(entry.getValue().getType(), entry.getKey());
        }

        return perms;
    }

    private static Entry<Method, Integer> getMethod(Method[] methods, String methodName, int toStart) {
        for (int i = toStart; i < methods.length; i++) {
            Method method = methods[i];

            if (!method.getName().equalsIgnoreCase(methodName))
                continue;

            return new HashMap.SimpleEntry(method, ++i);
        }
        return null;
    }

    private static HashMap<ArrayList<String>, Boolean> getOptions(String perm) {
        ArrayList<String> list = new ArrayList<>();
        boolean isRemove = true;
        String[] split = perm.split("\\.");

        for (int i = 1; i < split.length; i++) {
            String option = split[i];
            boolean value = option.startsWith("-");

            if (value) {
                option = option.substring(1);
                isRemove = false;
            }

            if (option.equals("baby")) {
                option = "setbaby";
            }

            list.add(option);
        }

        HashMap<ArrayList<String>, Boolean> options = new HashMap<>();
        options.put(list, isRemove);

        return options;
    }

    /**
     * Get perms for the node. Returns a hashmap of allowed disguisetypes and their options
     */
    public static HashMap<DisguisePerm, HashMap<ArrayList<String>, Boolean>> getPermissions(CommandSender sender,
            String permissionNode) {
        HashMap<DisguisePerm, HashMap<ArrayList<String>, Boolean>> singleDisguises = new HashMap<>();
        HashMap<DisguisePerm, HashMap<ArrayList<String>, Boolean>> rangeDisguises = new HashMap<>();
        HashMap<String, Boolean> perms = new HashMap<>();

        for (PermissionAttachmentInfo permission : sender.getEffectivePermissions()) {
            String perm = permission.getPermission().toLowerCase();

            if (perm.startsWith(permissionNode) && (!perms.containsKey(perm) || !permission.getValue())) {
                perms.put(perm, permission.getValue());
            }
        }

        if (!perms.containsKey(permissionNode + "*") && sender.hasPermission(permissionNode + "*")) {
            perms.put(permissionNode + "*", true);
        }

        if (!perms.containsKey(permissionNode + "*.*") && sender.hasPermission(permissionNode + "*.*")) {
            perms.put(permissionNode + "*.*", true);
        }

        for (String perm : perms.keySet()) {
            if (perms.get(perm)) {
                perm = perm.substring(permissionNode.length());

                String disguiseType = perm.split("\\.")[0];
                DisguisePerm dPerm = DisguiseParser.getDisguisePerm(disguiseType);

                if (dPerm != null) {
                    HashMap<ArrayList<String>, Boolean> list;

                    if (singleDisguises.containsKey(dPerm)) {
                        list = singleDisguises.get(dPerm);
                    } else {
                        list = new HashMap<>();
                        singleDisguises.put(dPerm, list);
                    }

                    HashMap<ArrayList<String>, Boolean> map1 = getOptions(perm);
                    list.put(map1.keySet().iterator().next(), map1.values().iterator().next());
                } else {
                    for (DisguisePerm type : getDisguisePerms()) {
                        HashMap<ArrayList<String>, Boolean> options = null;
                        Class entityClass = type.getEntityClass();

                        if (disguiseType.equals("mob")) {
                            if (type.isMob()) {
                                options = getOptions(perm);
                            }
                        } else if (disguiseType.equals("animal") || disguiseType.equals("animals")) {
                            if (Animals.class.isAssignableFrom(entityClass)) {
                                options = getOptions(perm);
                            }
                        } else if (disguiseType.equals("monster") || disguiseType.equals("monsters")) {
                            if (Monster.class.isAssignableFrom(entityClass)) {
                                options = getOptions(perm);
                            }
                        } else if (disguiseType.equals("misc")) {
                            if (type.isMisc()) {
                                options = getOptions(perm);
                            }
                        } else if (disguiseType.equals("ageable")) {
                            if (Ageable.class.isAssignableFrom(entityClass)) {
                                options = getOptions(perm);
                            }
                        } else if (disguiseType.equals("*")) {
                            options = getOptions(perm);
                        }

                        if (options != null) {
                            HashMap<ArrayList<String>, Boolean> list;

                            if (rangeDisguises.containsKey(type)) {
                                list = rangeDisguises.get(type);
                            } else {
                                list = new HashMap<>();
                                rangeDisguises.put(type, list);
                            }

                            HashMap<ArrayList<String>, Boolean> map1 = getOptions(perm);

                            list.put(map1.keySet().iterator().next(), map1.values().iterator().next());
                        }
                    }
                }
            }
        }

        for (String perm : perms.keySet()) {
            if (!perms.get(perm)) {
                perm = perm.substring(permissionNode.length());

                String disguiseType = perm.split("\\.")[0];
                DisguisePerm dType = DisguiseParser.getDisguisePerm(disguiseType);

                if (dType != null) {
                    singleDisguises.remove(dType);
                    rangeDisguises.remove(dType);
                } else {
                    for (DisguisePerm type : getDisguisePerms()) {
                        boolean foundHim = false;
                        Class entityClass = type.getEntityClass();

                        switch (disguiseType) {
                            case "mob":
                                if (type.isMob()) {
                                    foundHim = true;
                                }

                                break;
                            case "animal":
                            case "animals":
                                if (Animals.class.isAssignableFrom(entityClass)) {
                                    foundHim = true;
                                }

                                break;
                            case "monster":
                            case "monsters":
                                if (Monster.class.isAssignableFrom(entityClass)) {
                                    foundHim = true;
                                }

                                break;
                            case "misc":
                                if (type.isMisc()) {
                                    foundHim = true;
                                }

                                break;
                            case "ageable":
                                if (Ageable.class.isAssignableFrom(entityClass)) {
                                    foundHim = true;
                                }

                                break;
                            case "*":
                                foundHim = true;
                                break;
                        }

                        if (foundHim) {
                            rangeDisguises.remove(type);
                        }
                    }
                }
            }
        }

        HashMap<DisguisePerm, HashMap<ArrayList<String>, Boolean>> map = new HashMap<>();

        for (DisguisePerm type : getDisguisePerms()) {
            HashMap<ArrayList<String>, Boolean> temp = new HashMap<>();

            if (singleDisguises.containsKey(type)) {
                temp.putAll(singleDisguises.get(type));
            }

            if (rangeDisguises.containsKey(type)) {
                temp.putAll(rangeDisguises.get(type));
            }

            if (!temp.isEmpty()) {
                map.put(type, temp);
            }
        }

        return map;
    }

    private static boolean isDouble(String string) {
        try {
            Float.parseFloat(string);
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }

    private static boolean isInteger(String string) {
        try {
            Integer.parseInt(string);
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }

    /**
     * Splits a string while respecting quotes
     */
    public static String[] split(String string) {
        String[] strings = ChatColor.translateAlternateColorCodes('&', string).split(" ");

        ArrayList<String> list = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        for (String s : strings) {
            String a = s.replaceAll("\\\\", "");

            if (builder.length() != 0 || s.startsWith("\"")) {
                if (builder.length() != 0)
                    builder.append(' ');

                String append;

                if (s.startsWith("\""))
                    builder.append(s.substring(1));
                else if (a.endsWith("\"") && !a.endsWith("\\\""))
                    builder.append(s.substring(0, s.length() - 1));
                else
                    builder.append(s);

                if (a.endsWith("\"") && !a.endsWith("\\\"")) {
                    list.add(builder.toString());
                    builder = new StringBuilder();
                }
            } else {
                list.add(s);
            }
        }

        if (builder.length() != 0) {
            list.addAll(Arrays.asList(("\"" + builder.toString()).split(" ")));
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Returns the disguise if it all parsed correctly. Returns a exception with a complete message if it didn't. The
     * commandsender is purely used for checking permissions. Would defeat the purpose otherwise. To reach this
     * point, the
     * disguise has been feed a proper disguisetype.
     */
    public static Disguise parseDisguise(CommandSender sender, String permNode, String[] args,
            HashMap<DisguisePerm, HashMap<ArrayList<String>, Boolean>> permissionMap) throws DisguiseParseException,
            IllegalAccessException, InvocationTargetException {
        if (sender instanceof Player) {
            DisguiseUtilities.setCommandsUsed();
        }

        if (permissionMap.isEmpty()) {
            throw new DisguiseParseException(LibsMsg.NO_PERM);
        }

        if (args.length == 0) {
            throw new DisguiseParseException(LibsMsg.PARSE_NO_ARGS);
        }

        // How many args to skip due to the disugise being constructed
        // Time to start constructing the disguise.
        // We will need to check between all 3 kinds of disguises
        int toSkip = 1;
        ArrayList<String> usedOptions = new ArrayList<>();
        Disguise disguise = null;
        HashMap<ArrayList<String>, Boolean> optionPermissions;

        if (args[0].startsWith("@")) {
            if (sender.hasPermission("libsdisguises.disguise.disguiseclone")) {
                disguise = DisguiseUtilities.getClonedDisguise(args[0].toLowerCase());

                if (disguise == null) {
                    throw new DisguiseParseException(LibsMsg.PARSE_NO_REF, args[0]);
                }
            } else {
                throw new DisguiseParseException(LibsMsg.PARSE_NO_PERM_REF);
            }

            optionPermissions = (permissionMap.containsKey(new DisguisePerm(disguise.getType())) ?
                    permissionMap.get(new DisguisePerm(disguise.getType())) :
                    new HashMap<ArrayList<String>, Boolean>());
        } else {
            DisguisePerm disguisePerm = getDisguisePerm(args[0]);
            Entry<String, Disguise> customDisguise = DisguiseConfig.getCustomDisguise(args[0]);

            if (customDisguise != null) {
                disguise = customDisguise.getValue().clone();
            }

            if (disguisePerm == null) {
                throw new DisguiseParseException(LibsMsg.PARSE_DISG_NO_EXIST, args[0]);
            }

            if (disguisePerm.isUnknown()) {
                throw new DisguiseParseException(LibsMsg.PARSE_CANT_DISG_UNKNOWN);
            }

            if (disguisePerm.getEntityType() == null) {
                throw new DisguiseParseException(LibsMsg.PARSE_CANT_LOAD);
            }

            if (!permissionMap.containsKey(disguisePerm)) {
                throw new DisguiseParseException(LibsMsg.NO_PERM_DISGUISE);
            }

            optionPermissions = permissionMap.get(disguisePerm);

            HashMap<String, Boolean> disguiseOptions = getDisguiseOptions(sender, permNode, disguisePerm);

            if (disguise == null) {
                if (disguisePerm.isPlayer()) {
                    // If he is doing a player disguise
                    if (args.length == 1) {
                        // He needs to give the player name
                        throw new DisguiseParseException(LibsMsg.PARSE_SUPPLY_PLAYER);
                    } else {
                        if (!disguiseOptions.isEmpty() && (!disguiseOptions.containsKey(args[1].toLowerCase()) ||
                                !disguiseOptions.get(args[1].toLowerCase()))) {
                            throw new DisguiseParseException(LibsMsg.PARSE_NO_PERM_NAME);
                        }

                        args[1] = args[1].replace("\\_", " ");

                        // Construct the player disguise
                        disguise = new PlayerDisguise(ChatColor.translateAlternateColorCodes('&', args[1]));
                        toSkip++;
                    }
                } else if (disguisePerm.isMob()) { // Its a mob, use the mob constructor
                    boolean adult = true;

                    if (args.length > 1) {
                        if (args[1].equalsIgnoreCase(TranslateType.DISGUISE_OPTIONS.get("baby")) ||
                                args[1].equalsIgnoreCase(TranslateType.DISGUISE_OPTIONS.get("adult"))) {
                            usedOptions.add("setbaby");
                            doCheck(sender, optionPermissions, usedOptions);
                            adult = args[1].equalsIgnoreCase(TranslateType.DISGUISE_OPTIONS.get("adult"));

                            toSkip++;
                        }
                    }

                    disguise = new MobDisguise(disguisePerm.getType(), adult);
                } else if (disguisePerm.isMisc()) {
                    // Its a misc, we are going to use the MiscDisguise constructor.
                    int miscId = -1;
                    int miscData = -1;
                    String secondArg = null;

                    if (args.length > 1) {
                        // They have defined more arguments!
                        // If the first arg is a number
                        if (args[1].contains(":")) {
                            String[] split = args[1].split(":");
                            if (isInteger(split[1])) {
                                secondArg = split[1];
                            }
                            args[1] = split[0];
                        }

                        if (isInteger(args[1])) {
                            miscId = Integer.parseInt(args[1]);
                        } else {
                            if (disguisePerm.getType() == DisguiseType.FALLING_BLOCK ||
                                    disguisePerm.getType() == DisguiseType.DROPPED_ITEM) {
                                for (Material mat : Material.values()) {
                                    if (mat.name().replace("_", "").equalsIgnoreCase(args[1].replace("_", ""))) {
                                        miscId = mat.getId();
                                        break;
                                    }
                                }
                            }
                        }
                        if (miscId != -1) {
                            switch (disguisePerm.getType()) {
                                case PAINTING:
                                case FALLING_BLOCK:
                                case SPLASH_POTION:
                                case DROPPED_ITEM:
                                case FISHING_HOOK:
                                case ARROW:
                                case TIPPED_ARROW:
                                case SPECTRAL_ARROW:
                                case SMALL_FIREBALL:
                                case FIREBALL:
                                case WITHER_SKULL:
                                    break;
                                default:
                                    throw new DisguiseParseException(LibsMsg.PARSE_TOO_MANY_ARGS,
                                            disguisePerm.toReadable(), args[1]);
                            }
                            toSkip++;
                            // If they also defined a data value
                            if (args.length > 2 && secondArg == null && isInteger(args[2])) {
                                secondArg = args[2];
                                toSkip++;
                            }
                            if (secondArg != null) {
                                if (disguisePerm.getType() != DisguiseType.FALLING_BLOCK &&
                                        disguisePerm.getType() != DisguiseType.DROPPED_ITEM) {
                                    throw new DisguiseParseException(LibsMsg.PARSE_USE_SECOND_NUM,
                                            DisguiseType.FALLING_BLOCK.toReadable(),
                                            DisguiseType.DROPPED_ITEM.toReadable());
                                }
                                miscData = Integer.parseInt(secondArg);
                            }
                        }
                    }

                    if (!disguiseOptions.isEmpty() && miscId != -1) {
                        String toCheck = "" + miscId;

                        if (miscData == 0 || miscData == -1) {
                            if (!disguiseOptions.containsKey(toCheck) || !disguiseOptions.get(toCheck)) {
                                toCheck += ":0";
                            }
                        } else {
                            toCheck += ":" + miscData;
                        }

                        if (!disguiseOptions.containsKey(toCheck) || !disguiseOptions.get(toCheck)) {
                            throw new DisguiseParseException(LibsMsg.PARSE_NO_PERM_PARAM, toCheck,
                                    disguisePerm.toReadable());
                        }
                    }

                    if (miscId != -1) {
                        if (disguisePerm.getType() == DisguiseType.FALLING_BLOCK) {
                            usedOptions.add("setblock");

                            doCheck(sender, optionPermissions, usedOptions);
                        } else if (disguisePerm.getType() == DisguiseType.PAINTING) {
                            usedOptions.add("setpainting");

                            doCheck(sender, optionPermissions, usedOptions);
                        } else if (disguisePerm.getType() == DisguiseType.SPLASH_POTION) {
                            usedOptions.add("setpotionid");

                            doCheck(sender, optionPermissions, usedOptions);
                        }
                    }
                    // Construct the disguise
                    disguise = new MiscDisguise(disguisePerm.getType(), miscId, miscData);
                }
            }
        }

        // Copy strings to their new range
        String[] newArgs = new String[args.length - toSkip];
        System.arraycopy(args, toSkip, newArgs, 0, args.length - toSkip);

        callMethods(sender, disguise, optionPermissions, usedOptions, newArgs);

        // Alright. We've constructed our disguise.
        return disguise;
    }

    public static void callMethods(CommandSender sender, Disguise disguise,
            HashMap<ArrayList<String>, Boolean> optionPermissions, ArrayList<String> usedOptions,
            String[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            DisguiseParseException {
        Method[] methods = ReflectionFlagWatchers.getDisguiseWatcherMethods(disguise.getWatcher().getClass());

        for (int i = 0; i < args.length; i += 2) {
            String methodNameRaw;
            String methodName = TranslateType.DISGUISE_OPTIONS.reverseGet(methodNameRaw = args[i]);
            String valueString = TranslateType.DISGUISE_OPTIONS_PARAMETERS
                    .reverseGet(args.length - 1 == i ? null : args[i + 1]);
            Method methodToUse = null;
            Object value = null;
            DisguiseParseException storedEx = null;
            int c = 0;

            while (c < methods.length) {
                try {
                    Entry<Method, Integer> entry = getMethod(methods, methodName, c);

                    if (entry == null) {
                        break;
                    }

                    methodToUse = entry.getKey();
                    c = entry.getValue();
                    methodName = TranslateType.DISGUISE_OPTIONS.reverseGet(methodNameRaw = methodToUse.getName());
                    Class<?>[] types = methodToUse.getParameterTypes();
                    Class param = types[0];

                    if (valueString != null) {
                        if (int.class == param) {
                            // Parse to integer
                            if (isInteger(valueString)) {
                                value = Integer.parseInt(valueString);
                            } else {
                                throw parseToException(param, valueString, methodName);
                            }
                        } else if (WrappedGameProfile.class == param && valueString.length() > 20) {
                            try {
                                value = DisguiseUtilities.getGson().fromJson(valueString, WrappedGameProfile.class);
                            }
                            catch (Exception ex) {
                                throw parseToException(WrappedGameProfile.class, valueString, methodName);
                            }
                        } else if (float.class == param || double.class == param) {
                            // Parse to number
                            if (isDouble(valueString)) {
                                float obj = Float.parseFloat(valueString);
                                if (param == float.class) {
                                    value = obj;
                                } else if (param == double.class) {
                                    value = (double) obj;
                                }
                            } else {
                                throw parseToException(param, valueString, methodName);
                            }
                        } else if (param == String.class) {
                            if (methodNameRaw.equalsIgnoreCase("setskin") && valueString.length() > 20) {
                                value = valueString;
                            } else {
                                // Parse to string
                                value = ChatColor.translateAlternateColorCodes('&', valueString);
                            }
                        } else if (param == EulerAngle.class) {
                            String[] split = valueString.split(",");

                            if (split.length != 3)
                                throw parseToException(param, valueString, methodName);

                            if (!isDouble(split[0]) || !isDouble(split[1]) || !isDouble(split[2]))
                                throw parseToException(param, valueString, methodName);

                            value = new EulerAngle(Double.parseDouble(split[0]), Double.parseDouble(split[1]),
                                    Double.parseDouble(split[2]));
                        } else if (param == Villager.Profession.class) {
                            try {
                                value = Villager.Profession.valueOf(valueString.toUpperCase());
                            }
                            catch (Exception ex) {
                                throw parseToException(param, valueString, methodName);
                            }
                        } else if (param == AnimalColor.class) {
                            // Parse to animal color
                            try {
                                value = AnimalColor.valueOf(valueString.toUpperCase());
                            }
                            catch (Exception ex) {
                                throw parseToException(param, valueString, methodName);
                            }
                        } else if (param.getName().equals("org.bukkit.entity.Llama$Color")) {
                            try {
                                value = Llama.Color.valueOf(valueString.toUpperCase());
                            }
                            catch (Exception ex) {
                                throw parseToException(param, valueString, methodName);
                            }
                        } else if (param == ItemStack.class) {
                            // Parse to itemstack
                            value = parseToItemstack(param, methodName, valueString);
                        } else if (param == ItemStack[].class) {
                            // Parse to itemstack array
                            ItemStack[] items = new ItemStack[4];

                            String[] split = valueString.split(",", -1);

                            if (split.length == 4) {
                                for (int a = 0; a < 4; a++) {
                                    try {
                                        items[a] = parseToItemstack(param, methodName, split[a]);
                                    }
                                    catch (Exception ex) {
                                        throw parseToException(param, valueString, methodName);
                                    }
                                }
                            } else {
                                throw parseToException(param, valueString, methodName);
                            }

                            value = items;
                        } else if (param == Horse.Color.class) {
                            // Parse to horse color
                            value = callValueOf(param, valueString, methodName);
                        } else if (param == Horse.Style.class) {
                            // Parse to horse style
                            value = callValueOf(param, valueString, methodName);
                        } else if (param == Art.class) {
                            // Parse to art type
                            value = callValueOf(param, valueString, methodName);
                        } else if (param == Ocelot.Type.class) {
                            // Parse to ocelot type
                            value = callValueOf(param, valueString, methodName);
                        } else if (param.getSimpleName().equals("TreeSpecies")) {
                            // Parse to tree species
                            value = callValueOf(param, valueString, methodName);
                        } else if (param == PotionEffectType.class) {
                            // Parse to potion effect
                            try {
                                PotionEffectType potionType = PotionEffectType.getByName(valueString.toUpperCase());

                                if (potionType == null && isInteger(valueString)) {
                                    potionType = PotionEffectType.getById(Integer.parseInt(valueString));
                                }

                                if (potionType == null) {
                                    throw new DisguiseParseException();
                                }

                                value = potionType;
                            }
                            catch (Exception ex) {
                                throw parseToException(param, valueString, methodName);
                            }
                        } else if (param == int[].class) {
                            String[] split = valueString.split(",");

                            int[] values = new int[split.length];

                            for (int b = 0; b < values.length; b++) {
                                try {
                                    values[b] = Integer.parseInt(split[b]);
                                }
                                catch (NumberFormatException ex) {
                                    throw parseToException(param, valueString, methodName);
                                }
                            }

                            value = values;
                        } else if (param == BlockFace.class) {
                            try {
                                BlockFace face = BlockFace.valueOf(valueString.toUpperCase());

                                if (face.ordinal() > 5) {
                                    throw new DisguiseParseException();
                                }

                                value = face;
                            }
                            catch (Exception ex) {
                                throw parseToException(param, valueString, methodName);
                            }
                        } else if (param == RabbitType.class) {
                            try {
                                for (RabbitType type : RabbitType.values()) {
                                    if (type.name().replace("_", "")
                                            .equalsIgnoreCase(valueString.replace("_", "").replace(" ", ""))) {
                                        value = type;

                                        break;
                                    }
                                }
                                if (value == null) {
                                    throw new Exception();
                                }
                            }
                            catch (Exception ex) {
                                throw parseToException(param, valueString, methodName);
                            }
                        } else if (param == BlockPosition.class) {
                            try {
                                String[] split = valueString.split(",");

                                assert split.length == 3;

                                value = new BlockPosition(Integer.parseInt(split[0]), Integer.parseInt(split[1]),
                                        Integer.parseInt(split[2]));
                            }
                            catch (Exception ex) {
                                throw parseToException(param, valueString, methodName);
                            }
                        } else if (param.getName().equals("org.bukkit.entity.Parrot$Variant")) {
                            value = callValueOf(param, valueString, methodName);
                        }
                    }

                    if (value == null && boolean.class == param) {
                        if (valueString == null) {
                            value = true;
                            i--;
                        } else if (valueString
                                .equalsIgnoreCase(TranslateType.DISGUISE_OPTIONS_PARAMETERS.get("true"))) {
                            value = true;
                        } else if (valueString
                                .equalsIgnoreCase(TranslateType.DISGUISE_OPTIONS_PARAMETERS.get("false"))) {
                            value = false;
                        } else {
                            if (getMethod(methods, valueString, 0) == null) {
                                throw parseToException(param, valueString, methodName);
                            } else {
                                value = true;
                                i--;
                            }
                        }
                    }

                    if (value != null) {
                        break;
                    }
                }
                catch (DisguiseParseException ex) {
                    storedEx = ex;
                    methodToUse = null;
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    methodToUse = null;
                }
            }

            if (methodToUse == null) {
                if (storedEx != null) {
                    throw storedEx;
                }

                throw new DisguiseParseException(LibsMsg.PARSE_OPTION_NA, methodName);
            }
            if (value == null) {
                throw new DisguiseParseException(LibsMsg.PARSE_NO_OPTION_VALUE, methodName);
            }

            if (!usedOptions.contains(methodNameRaw.toLowerCase())) {
                usedOptions.add(methodNameRaw.toLowerCase());
            }

            doCheck(sender, optionPermissions, usedOptions);

            if (FlagWatcher.class.isAssignableFrom(methodToUse.getDeclaringClass())) {
                methodToUse.invoke(disguise.getWatcher(), value);
            } else {
                methodToUse.invoke(disguise, value);
            }
        }
    }

    private static DisguiseParseException parseToException(Class paramType, String receivedInstead, String methodName) {
        return new DisguiseParseException(LibsMsg.PARSE_EXPECTED_RECEIVED,
                ReflectionFlagWatchers.getParamInfo(paramType).getName(), receivedInstead, methodName);
    }

    private static ItemStack parseToItemstack(Class param, String method, String string) throws DisguiseParseException {
        String[] split = string.split(":", -1);

        int itemId = -1;

        if (split[0].isEmpty() || split[0].equalsIgnoreCase("null")) {
            return null;
        } else if (isInteger(split[0])) {
            itemId = Integer.parseInt(split[0]);
        } else {
            try {
                itemId = Material.valueOf(split[0].toUpperCase()).getId();
            }
            catch (Exception ignored) {
            }
        }

        if (itemId != -1) {
            short itemDura = 0;

            if (split.length > 1) {
                if (isInteger(split[1])) {
                    itemDura = Short.parseShort(split[1]);
                } else {
                    throw parseToException(param, string, "%s");
                }
            }

            return new ItemStack(itemId, 1, itemDura);
        } else {
            if (split.length == 1) {
                throw parseToException(param, string, "%s");
            } else {
                throw parseToException(param, string, "%s");
            }
        }
    }

    public static boolean passesCheck(CommandSender sender, HashMap<ArrayList<String>, Boolean> theirPermissions,
            ArrayList<String> usedOptions) {
        if (theirPermissions == null)
            return false;

        boolean hasPermission = false;

        for (ArrayList<String> list : theirPermissions.keySet()) {
            boolean myPerms = true;

            for (String option : usedOptions) {
                if (!sender.getName().equals("CONSOLE") && option.equalsIgnoreCase("setInvisible") &&
                        DisguiseConfig.isDisabledInvisibility()) {
                    myPerms = false;
                }

                if (!(theirPermissions.get(list) && list.contains("*")) &&
                        (list.contains(option) != theirPermissions.get(list))) {
                    myPerms = false;
                    break;
                }
            }

            if (myPerms) {
                hasPermission = true;
            }
        }

        return hasPermission;
    }
}
