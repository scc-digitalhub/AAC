import { Form, SaveButton, TextInput } from 'react-admin';
import { DialogActions, Stack } from '@mui/material';
import { Page } from '../components/Page';

export const GroupCreateForm = () => {
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

                    <TextInput
                        source="group"
                        label="field.group.name"
                        helperText="field.group.helperText"
                        fullWidth
                    />
                </Stack>
            </Page>
            <DialogActions>
                <SaveButton label="ra.action.create" variant="text" />
            </DialogActions>
        </Form>
    );
};
