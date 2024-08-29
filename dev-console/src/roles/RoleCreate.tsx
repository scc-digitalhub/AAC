import { Form, SaveButton, TextInput } from 'react-admin';
import { DialogActions, Stack } from '@mui/material';
import { Page } from '../components/Page';

export const RoleCreateForm = () => {
    return (
        <Form>
            <Page>
                <Stack rowGap={2}>
                    <TextInput
                        source="name"
                        label="field.role.name"
                        helperText="field.name.helperText"
                        fullWidth
                        required
                    />

                    <TextInput
                        source="role"
                        label="field.role.name"
                        helperText="field.role.helperText"
                        fullWidth
                        required
                    />
                </Stack>
            </Page>
            <DialogActions>
                <SaveButton label="ra.action.create" variant="text" />
            </DialogActions>
        </Form>
    );
};
