package fun.yeelo.oauth.utils;

import fun.yeelo.oauth.domain.share.Share;

public class UserContext {
    private static final ThreadLocal<Share> currentUser = new ThreadLocal<>();
    
    public static void setCurrentUser(Share user) {
        currentUser.set(user);
    }
    
    public static Share getCurrentUser() {
        return currentUser.get();
    }
    
    public static void clear() {
        currentUser.remove();
    }
} 