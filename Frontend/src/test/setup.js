// Node 25 exposes a non-functional `localStorage` global (requires
// --localstorage-file) that shadows jsdom's implementation under vitest.
// Replace it with a real in-memory Storage for tests.
class MemoryStorage {
  #store = new Map()

  getItem(key) {
    return this.#store.has(key) ? this.#store.get(key) : null
  }

  setItem(key, value) {
    this.#store.set(String(key), String(value))
  }

  removeItem(key) {
    this.#store.delete(key)
  }

  clear() {
    this.#store.clear()
  }

  get length() {
    return this.#store.size
  }

  key(index) {
    return [...this.#store.keys()][index] ?? null
  }
}

Object.defineProperty(globalThis, 'localStorage', {
  value: new MemoryStorage(),
  writable: true,
  configurable: true,
})
