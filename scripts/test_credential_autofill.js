/* Exercise the actual Kotlin-generated autofill script with a minimal DOM. */
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");
const source = fs.readFileSync("core/src/main/java/com/marble098/marblebr/core/model/LoginObservationProbe.kt", "utf8");
const match = source.match(/fun autofillScript\([\s\S]*?return """([\s\S]*?)"""\.trimIndent\(\)/);
assert.ok(match, "autofill script was not found");
const script = match[1].replace("$userLiteral", JSON.stringify("alice")).replace("$passLiteral", JSON.stringify("secret"));

function run({ action = "https://example.test/login", submitter = null, externalSubmitter = null, onUsernameInput = () => {}, withForm = true } = {}) {
  class Input {
    constructor() { this._value = ""; }
    get value() { return this._value; }
    set value(value) { this._value = value; }
    getClientRects() { return [1]; }
    closest() { return this.form; }
    dispatchEvent(event) { if (this === username && event.type === "input") onUsernameInput(form, password); }
  }
  const username = new Input();
  const password = new Input();
  const form = {
    action,
    querySelectorAll(selector) {
      return selector.startsWith("button") ? submitters.filter(button => !button.external) : [username];
    }
  };
  const submitters = [[submitter, false], [externalSubmitter, true]].filter(([target]) => target)
    .map(([target, external]) => ({ form, external, formAction: target, getAttribute: () => target }));
  username.form = password.form = withForm ? form : null;
  const document = {
    baseURI: "https://example.test/login",
    querySelectorAll(selector) {
      if (selector === "input[type=password]") return [password];
      if (selector.startsWith("button")) return submitters;
      return [username];
    }
  };
  const result = vm.runInNewContext(script, {
    document,
    window: { HTMLInputElement: Input },
    location: { origin: "https://example.test", href: "https://example.test/login" },
    URL,
    Event: class { constructor(type) { this.type = type; } },
    KeyboardEvent: class { constructor(type) { this.type = type; } }
  });
  return { result, user: username.value, password: password.value };
}

assert.deepEqual(run(), { result: "filled", user: "alice", password: "secret" });
assert.equal(run({ action: "/session" }).result, "filled");
assert.equal(run({ withForm: false }).result, "filled");
for (const action of ["https://evil.test/steal", "http://example.test/login", "https://example.test:8443/login", "javascript:alert(1)"]) {
  assert.deepEqual(run({ action }), { result: "unsafe-form", user: "", password: "" });
}
assert.equal(run({ submitter: "https://evil.test/steal" }).password, "");
assert.equal(run({ externalSubmitter: "https://evil.test/steal" }).password, "",
  "submitters associated with form= outside the form must also be checked");
assert.equal(run({ onUsernameInput: form => { form.action = "https://evil.test/steal"; } }).password, "");
assert.equal(run({ onUsernameInput: (form, password) => { password.form = { action: "https://evil.test/steal" }; } }).password, "",
  "the password field must not change form after username events");
assert.equal(run({ withForm: false, onUsernameInput: (form, password) => { password.form = form; } }).password, "",
  "an unassociated password field must not move into a form while filling");
console.log("Credential autofill form-origin tests passed.");
