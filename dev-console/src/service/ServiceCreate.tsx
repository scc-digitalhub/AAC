import { Form, SaveButton, TextInput } from 'react-admin';
import { DialogActions, Stack } from '@mui/material';
import { Page } from '../components/Page';

export const ServiceCreateForm = () => {
    return (
        <Form>
            <Page>
                <Stack rowGap={2}>
                    <TextInput
                        source="name"
                        label="field.name.name"
                        helperText="field.name.helperText"
                        fullWidth
                        required
                    />

                    <TextInput
                        source="namespace"
                        label="field.namespace.name"
                        helperText="field.namespace.helperText"
                        fullWidth
                        required
                        type={'url'}
                    />
                </Stack>
            </Page>
            <DialogActions>
                <SaveButton label="ra.action.create" variant="text" />
            </DialogActions>
        </Form>
    );
};
