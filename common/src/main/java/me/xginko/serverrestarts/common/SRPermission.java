package me.xginko.serverrestarts.common;

import org.bukkit.permissions.PermissionDefault;

public enum SRPermission {

    VERSION("serverrestarts.cmd.version", "Permission to get the plugin version", PermissionDefault.FALSE),
    RELOAD("serverrestarts.cmd.reload", "Permission to reload the plugin config", PermissionDefault.FALSE),
    DISABLE("serverrestarts.cmd.disable", "Permission to disable the plugin", PermissionDefault.FALSE),
    GRACEFUL("serverrestarts.cmd.restartgracefully", "Permission to schedule an immediate timed restart", PermissionDefault.FALSE);

    private final String permission, description;
    private final PermissionDefault permissionDefault;

    SRPermission(String permission, String description, PermissionDefault permissionDefault) {
        this.permission = permission;
        this.description = description;
        this.permissionDefault = permissionDefault;
    }

    public String permission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }

    public PermissionDefault getDefault() {
        return permissionDefault;
    }
}
