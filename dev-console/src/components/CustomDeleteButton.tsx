import React from 'react';
import {
    Button,
    Confirm,
    DeleteButton,
    DeleteWithConfirmButton,
    EditButton,
    ShowButton,
    SimpleForm,
    TextField,
    TextInput,
    required,
    useDelete,
    useNotify,
    useRecordContext,
    useRefresh,
} from 'react-admin';
import DeleteIcon from '@mui/icons-material/Delete';

export const CustomDeleteButton = (params: any) => {
    const record = useRecordContext();
    const notify = useNotify();
    const refresh = useRefresh();
    const [open, setOpen] = React.useState(false);
    const handleClick = () => setOpen(true);
    const [deleteOne, { isLoading }] = useDelete(
        'apps',
        { id: record.name, meta: { realmId: params.realmId, id: params.id } },
        {
            onSuccess: () => {
                notify(`Client application deleted successfully`);
                // redirect('list', 'monitor');
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
                {/* <Button disabled={isLoading} endIcon={<DeleteOutlineIcon sx={{ color: 'red' }} />} onClick={handleClick} /> */}
                {/* create library of custom edit dialog  */}
                <Button
                    label=""
                    endIcon={<DeleteIcon />}
                    onClick={handleClick}
                    sx={{ color: 'red' }}
                />
            </span>
            <Confirm
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
            <SimpleForm toolbar={false}>
                <TextInput label="Client Id*" source="selectedId" fullWidth />
            </SimpleForm>
        </>
    );
};
