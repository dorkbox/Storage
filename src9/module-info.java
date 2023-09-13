module dorkbox.storage {
    exports dorkbox.storage;
    exports dorkbox.storage.serializer;
    exports dorkbox.storage.types;

    requires transitive dorkbox.byteUtils;
    requires transitive dorkbox.json;
    requires transitive dorkbox.minlog;
    requires transitive dorkbox.objectpool;
    requires transitive dorkbox.serializers;
    requires transitive dorkbox.updates;

    requires transitive com.esotericsoftware.kryo;
    requires transitive org.slf4j;

    requires transitive kotlin.stdlib;
}
