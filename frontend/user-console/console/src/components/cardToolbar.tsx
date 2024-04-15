import { styled } from '@mui/material/styles';
import { Toolbar } from '@mui/material';
import { ToolbarProps } from 'react-admin';

export const CardToolbar = (props: ToolbarProps) => {
    const { children, className, resource, ...rest } = props;

    return (
        <StyledToolbar role="toolbar" className={className} {...rest}>
            {children}
        </StyledToolbar>
    );
};

const StyledToolbar = styled(Toolbar, {
    name: 'CardToolbar',
})(({ theme }) => ({
    flex: 1,
    display: 'flex',
    backgroundColor: theme.palette.grey[100],
}));

export default CardToolbar;
