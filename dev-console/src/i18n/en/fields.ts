export const fields = {
    id: {
        name: 'Id',
        helperText: 'Unique identifier',
    },
    name: {
        name: 'Name',
        helperText: 'Human readable name',
    },
    description: {
        name: 'Description',
        helperText: 'Text describing the element, used for views',
    },
    type: {
        name: 'Type',
        helperText: 'Type field',
    },
    clientId: {
        name: 'ClientId',
        helperText: 'Client (unique) identifier',
    },
    slug: {
        name: 'Slug',
        helperText: 'Path-style (unique) identifier',
    },
    public: {
        name: 'Public',
        helperText: 'Toggle for public/private visibility',
    },
    languages: {
        name: 'Languages',
        helperText: 'List of languages (codes) enabled',
    },
    applicationType: {
        name: 'Application type',
        helperText: 'Type of (OAuth2) application',
    },
    title: {
        name: 'Title',
        helperText: 'Title used for views',
    },
    customStyle: {
        name: 'Custom style',
        helperText: 'Define a custom style sheet (CSS) to be injected in pages',
    },
};

export default fields;
