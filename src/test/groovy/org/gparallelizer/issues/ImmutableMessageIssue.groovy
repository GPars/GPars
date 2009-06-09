package org.gparallelizer.issues

final @Immutable class TestMessage {
    String value
}

final class Foo {}

if (new Foo() == new TestMessage('Value')) println 'Equal'
if (new TestMessage('Value') == new Foo()) println 'Equal'
