package groovyx.gpars.remote

class LocalHostMock extends LocalHost {

    @Override
    def <T> void registerProxy(Class<T> klass, String name, T object) {

    }

    @Override
    def <T> T get(Class<T> klass, String name) {
        return null
    }
}
