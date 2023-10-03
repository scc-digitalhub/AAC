import validator from '@rjsf/validator-ajv8';
import Form from '@rjsf/core';
// import Form from '@rjsf/material-ui';
import { RJSFSchema } from '@rjsf/utils';
import { EditGuesser } from 'react-admin';

const log = (type: any) => console.log.bind(console, type);

export const JSONSchemaForm = (params: any) => {
    return (
        <>
            <Form
                schema={params.schema}
                uiSchema={params.uiSchema}
                formData={params.record}
                validator={validator}
                onChange={log('changed')}
                onSubmit={log('submitted')}
                onError={log('errors')}
            />
        </>
    );
};
