require('process');
const _ = require('lodash');
const { readFileSync, writeFileSync } = require('fs');
const { parseArgs } = require('node:util');
const readline = require('readline');

let obj = {};
let defaultValue = '';
let prefix = null;
let output = null;

const args = parseArgs({
    options: {
        file: {
            type: 'string',
            short: 'f',
        },
        defaultValue: {
            type: 'string',
            short: 'd',
        },
        prefix: {
            type: 'string',
            short: 'p',
        },
        output: {
            type: 'string',
            short: 'o',
        },
    },
});

if (args.values.file) {
    //read json from file as obj
    const data = readFileSync(args.values.file);
    if (data) {
        obj = JSON.parse(data);
    }
}

if (args.values.defaultValue) {
    defaultValue = args.values.defaultValue;
}
if (args.values.prefix) {
    prefix = args.values.prefix;
}
if (args.values.output) {
    output = args.values.output;
}

(async function () {
    for await (const line of readline.createInterface({
        input: process.stdin,
    })) {
        let input = line.replace(/(\r\n|\n|\r)/gm, '');
        if (prefix) {
            input = input.substring(prefix.length + 1);
        }

        if (!_.get(obj, input)) {
            const value = defaultValue ? defaultValue : input;
            _.set(obj, input, value);
        }
    }

    const result = JSON.stringify(obj, null, 4);
    if (output) {
        console.log(`write result to ${output}`);
        writeFileSync(output, result, { encoding: 'utf8' });
    } else {
        process.stdout.write(result);
    }
})();
