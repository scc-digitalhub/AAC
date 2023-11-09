import YAML from 'yaml';
export const YamlExporter = (
    data: any[],
    arg1: any,
    agr2: any,
    resource: any
) => {
    const yaml = YAML.stringify({ resource: data });
    var blob = new Blob([yaml], { type: 'application/yaml' });
    let url = window.URL.createObjectURL(blob);

    // Creating the hyperlink and auto click it to start the download
    let link = document.createElement('a');
    link.href = url;
    link.download = resource + '.yml';
    link.click();
};
