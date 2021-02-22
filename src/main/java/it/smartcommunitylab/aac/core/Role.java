package it.smartcommunitylab.aac.core;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;

public class Role implements GrantedAuthority {

    private static final long serialVersionUID = 7746685141380346961L;

    // role definition
    private String context;
    private String space;
    private String role;

    public Role(String context, String space, String role) {
        super();
        this.context = context;
        this.space = space;
        this.role = role;
        validate(this);
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "Role [context=" + context + ", space=" + space + ", role=" + role + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        result = prime * result + ((space == null) ? 0 : space.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Role other = (Role) obj;
        if (context == null) {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        if (role == null) {
            if (other.role != null)
                return false;
        } else if (!role.equals(other.role))
            return false;
        if (space == null) {
            if (other.space != null)
                return false;
        } else if (!space.equals(other.space))
            return false;
        return true;
    }

    public String canonicalSpace() {
        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(context)) {
            sb.append(context);
            sb.append('/');
        }
        if (!StringUtils.isEmpty(space)) {
            sb.append(space);
        }
        return sb.toString();
    }

    public String asSlug() {
        return canonicalSpace().replace('/', '-');
    }

    @Override
    public String getAuthority() {
        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(context)) {
            sb.append(context);
            sb.append('/');
        }
        if (!StringUtils.isEmpty(space)) {
            sb.append(space);
            sb.append(':');
        }
        sb.append(role);
        return sb.toString();
    }

    public static Role systemUser() {
        return new Role(null, null, Config.R_USER);
    }

    public static Role systemAdmin() {
        return new Role(null, null, Config.R_ADMIN);
    }

    public static Role systemDeveloper() {
        return new Role(null, null, Config.R_DEVELOPER);
    }

    public static Role ownerOf(String ctxStr) {
        int idx = ctxStr.lastIndexOf('/');
        String ctx = idx > 0 ? ctxStr.substring(0, idx) : null;
        String space = idx > 0 ? ctxStr.substring(idx + 1) : ctxStr;
        return new Role(ctx, space, Config.R_PROVIDER);
    }

    public static Role memberOf(String ctxStr, String role) {
        int idx = ctxStr.lastIndexOf('/');
        String ctx = idx > 0 ? ctxStr.substring(0, idx) : null;
        String space = idx > 0 ? ctxStr.substring(idx + 1) : ctxStr;
        Role r = new Role(ctx, space, role);
        validate(r);
        return r;
    }

    public static Role parse(String s) throws IllegalArgumentException {
        s = s.trim();
        int idx = s.lastIndexOf(':');
        if (StringUtils.isEmpty(s) || idx == s.length() - 1)
            throw new IllegalArgumentException("Invalid Role format " + s);
        if (idx <= 0)
            return new Role(null, null, s.substring(idx + 1));
        return memberOf(s.substring(0, idx), s.substring(idx + 1));
    }

    public static void validate(Role r) throws IllegalArgumentException {
        // context may be empty
        if (r.context != null && !r.context.matches("[\\w\\./]+")) {
            throw new IllegalArgumentException(
                    "Invalid role context value: only alpha-numeric characters and '_./' allowed");
        }

        // space empty only if context is empty
        if (r.space == null && r.context != null || r.space != null && !r.space.matches("[\\w\\.]+")) {
            throw new IllegalArgumentException(
                    "Invalid role space value: only alpha-numeric characters and '_.' allowed");
        }

        // role should never be empty
        if (r.role == null || !r.role.matches("[\\w\\.]+")) {
            throw new IllegalArgumentException("Invalid role value: only alpha-numeric characters and '_.' allowed");
        }

    }

}