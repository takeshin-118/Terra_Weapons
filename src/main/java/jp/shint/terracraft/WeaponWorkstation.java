package jp.shint.terracraft;

/** Shared server validation and synchronized client feedback for vanilla workstations. */
public interface WeaponWorkstation {
    int NONE = 0;
    int PERMISSION_REQUIRED = 1;
    int TOO_HARD = 2;
    boolean terracraft$allowed();
    int terracraft$problem();
    void terracraft$refreshPermission();
}
