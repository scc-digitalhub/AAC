import YAML from 'yaml';
export const YamlExporter = (
    data: any[],
    arg1: any,
    agr2: any,
    resource: any
) => {
    let dataObj: any = {};
    dataObj[resource] = data;
    const yaml = YAML.stringify(dataObj);
    var blob = new Blob([yaml], { type: 'application/yaml' });
    let url = window.URL.createObjectURL(blob);

    // Creating the hyperlink and auto click it to start the download
    let link = document.createElement('a');
    link.href = url;
    link.download = resource + '.yml';
    link.click();
};
