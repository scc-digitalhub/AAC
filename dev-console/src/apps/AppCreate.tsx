import { Form, SaveButton, SelectInput, TextInput } from 'react-admin';
import { DialogActions, Stack } from '@mui/material';
import { Page } from '../components/Page';
import { schemaOAuthClient } from './schemas';

export const AppCreateForm = () => {
    const types = schemaOAuthClient.properties?.applicationType['enum'] || [
        'web',
    ];

    return (
        <Form>
            <Page>
                <Stack rowGap={2}>
                    <TextInput
                        source="name"
                        label="field.name.name"
                        helperText="field.name.helperText"
                        fullWidth
                    />

                    <SelectInput
                        source="configuration.applicationType"
                        label="field.applicationType.name"
                        helperText="field.applicationType.helperText"
                        choices={types.map(t => ({
                            id: t,
                            name: 'applicationType.' + t,
                        }))}
                        defaultValue={types[0]}
                    />
                </Stack>
            </Page>
            <DialogActions>
                <SaveButton label="ra.action.create" variant="text" />
            </DialogActions>
        </Form>
    );
};
