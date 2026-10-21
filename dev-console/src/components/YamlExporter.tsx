import YAML from 'yaml';

// list key written for each resource: the name used by RealmConfig and expected by the yaml import
const LIST_KEYS: Record<string, string> = {
    apps: 'clientApps',
    idps: 'identityProviders',
    aps: 'attributeProviders',
    attributeset: 'attributeSets',
};

export const YamlExporter = (
    data: any[],
    arg1: any,
    agr2: any,
    resource: any
) => {
    const key = LIST_KEYS[resource] || resource;
    let dataObj: any = {};
    dataObj[key] = data;
    const yaml = YAML.stringify(dataObj);
    var blob = new Blob([yaml], { type: 'application/yaml' });
    let url = window.URL.createObjectURL(blob);

    // Creating the hyperlink and auto click it to start the download
    let link = document.createElement('a');
    link.href = url;
    link.download = key + '.yml';
    link.click();
};
