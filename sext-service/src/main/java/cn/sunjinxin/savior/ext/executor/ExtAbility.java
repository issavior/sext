package cn.sunjinxin.savior.ext.executor;

import cn.sunjinxin.savior.ext.container.IExt;

/**
 * ext
 *
 * @author issavior
 */
public class ExtAbility {

    /**
     * builder
     *
     * @param clazz ability
     * @param <A>   /
     * @return ExtExecutor
     */
    public static <A extends IExt> ExtExecutor<A> as(Class<A> clazz) {
        return new ExtExecutor<>(clazz);
    }
}
