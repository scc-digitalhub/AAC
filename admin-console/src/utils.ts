export class utils {
    //deep copy an obj with nested props
    public static deepCopy<T>(source: T): T {
        return Array.isArray(source)
            ? source.map(i => this.deepCopy(i))
            : source && typeof source === 'object'
            ? Object.getOwnPropertyNames(source).reduce((obj, prop) => {
                  Object.defineProperty(
                      obj,
                      prop,
                      Object.getOwnPropertyDescriptor(source, prop)!
                  );
                  obj[prop] = this.deepCopy(
                      (source as { [key: string]: any })[prop]
                  );
                  return obj;
              }, Object.create(Object.getPrototypeOf(source)))
            : (source as T);
    }
}

export default utils;
