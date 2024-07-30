package fun.yeelo.oauth.enums;

public enum ExceptionTypeEnum {

    /**
     * 业务流程中的异常
     */
    BIZ_EXCEPTION(0, "业务异常"),

    /**
     * 数据库相关的异常
     */
    DB_EXCEPTION(1, "数据库异常"),

    /**
     * redis相关的异常
     */
    REDIS_EXCEPTION(2, "Redis异常"),

    /**
     * ElasticSearch相关的异常
     */
    ES_EXCEPTION(3, "ElasticSearch异常"),

    /**
     * 消息队列相关的异常
     */
    MQ_EXCEPTION(4, "消息队列异常"),

    /**
     * 系统抛出的不可预知的异常
     */
    SYSTEM_EXCEPTION(5, "系统异常"),

    /**
     * 前端请求异常
     */
    REQUEST_EXCEPTION(6, "前端请求异常");

    int type;

    String describe;

    ExceptionTypeEnum(int type, String describe) {
        this.type = type;
        this.describe = describe;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }
}
