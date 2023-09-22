import {
    Button,
    SimpleForm,
    TextField,
    TextInput,
    required,
    useDelete,
    useInput,
    useNotify,
    useRecordContext,
    useRedirect,
    useRefresh,
    useStore,
} from 'react-admin';
import DeleteIcon from '@mui/icons-material/Delete';
import { CustomDeleteConfirm } from './CustomDeleteConfirm';
import React from 'react';

export const CustomDeleteButton = (params: any) => {
    const redirect = useRedirect();
    const record = useRecordContext();
    const notify = useNotify();
    const refresh = useRefresh();
    const [open, setOpen] = React.useState(false);
    const [disabledeletebutton, setDisableDeleteButton] =
        useStore('delete.disabled');

    const handleClick = () => {
        setDisableDeleteButton(true);
        setOpen(true);
    };

    const [deleteOne, { isLoading }] = useDelete(
        'apps',
        { id: record.name, meta: { realmId: params.realmId, id: params.id } },
        {
            onSuccess: () => {
                notify(`Client application deleted successfully`);
                redirect('list', 'apps');
                refresh();
            },
        }
    );

    let title = 'Client Application Deletion';

    const handleDialogClose = () => setOpen(false);
    const handleConfirm = (data: any) => {
        deleteOne();
        setOpen(false);
    };

    const to = `/apps/r/${params.realmId}/${record.id}`;

    return (
        <>
            <span>
                <Button
                    label="Delete"
                    startIcon={<DeleteIcon />}
                    onClick={handleClick}
                    sx={{ color: 'red' }}
                />
            </span>
            <CustomDeleteConfirm
                disableDeleteButton={disabledeletebutton}
                isOpen={open}
                loading={isLoading}
                title={title}
                content={<DialogContent id={record.id} />}
                onConfirm={handleConfirm}
                onClose={handleDialogClose}
                onTouchCancel={handleDialogClose}
            />
        </>
    );
};

const DialogContent = (params: any) => {
    const [disabledeletebutton, setDisableDeleteButton] =
        useStore('delete.disabled');

    const idValidation = (value: any, allValues: any) => {
        if (value === params.id) {
            setDisableDeleteButton(false);
        } else {
            setDisableDeleteButton(true);
        }
        if (!value) {
            return 'The id is required';
        }
        if (value !== params.id) {
            return 'Id does not match';
        }
        return undefined;
    };

    const validateClientId = [required(), idValidation];

    return (
        <>
            <span>Are you sure you want to delete this application ?</span>
            <br />
            <br />
            <span>
                You are deleting client{' '}
                <span style={{ fontWeight: 500 }}>{params.id}</span>{' '}
            </span>
            <br />
            <span>To proceed enter the clientId</span>
            <br />
            <br />
            <span style={{ color: 'red' }}>
                ATTENTION: This operation cannot be undone!
            </span>
            <SimpleForm toolbar={false} mode="onChange" reValidateMode="onBlur">
                <TextInput
                    label="Client Id*"
                    source="selectedId"
                    validate={validateClientId}
                    fullWidth
                />
            </SimpleForm>
        </>
    );
};
